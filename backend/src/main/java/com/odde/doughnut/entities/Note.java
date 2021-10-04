package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.algorithms.SiblingOrder;
import com.odde.doughnut.entities.json.LinkViewed;
import com.odde.doughnut.entities.json.NotePositionViewedByUser;
import com.odde.doughnut.entities.json.NoteWithPosition;
import com.odde.doughnut.entities.json.NoteViewedByUser;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.WhereJoinTable;
import org.springframework.beans.BeanUtils;

import javax.persistence.*;
import javax.validation.Valid;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Entity
@Table(name = "note")
public class Note {

    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Embedded
    @Valid
    @Getter
    private final NoteContent noteContent = new NoteContent();

    @Column(name = "sibling_order")
    private Long siblingOrder = SiblingOrder.getGoodEnoughOrderNumber();

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "notebook_id", referencedColumnName = "id")
    @JsonIgnore
    @Getter
    private Notebook notebook;

    @Column(name = "created_at")
    @Getter
    @Setter
    private Timestamp createdAt;

    @OneToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "master_review_setting_id", referencedColumnName = "id")
    @JsonIgnore
    @Getter
    @Setter
    private ReviewSetting masterReviewSetting;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @JsonIgnore
    @Getter
    @Setter
    private User user;

    @OneToMany(mappedBy = "sourceNote", cascade = CascadeType.ALL,
            orphanRemoval = true)
    @JsonIgnore
    @Getter
    @Setter
    private List<Link> links = new ArrayList<>();

    @OneToMany(mappedBy = "targetNote", cascade = CascadeType.ALL,
            orphanRemoval = true)
    @JsonIgnore
    @Getter
    @Setter
    private List<Link> refers = new ArrayList<>();

    @OneToMany(mappedBy = "note", cascade = CascadeType.ALL)
    @JsonIgnore
    @OrderBy("depth DESC")
    @Getter
    @Setter
    private List<NotesClosure> notesClosures = new ArrayList<>();

    @OneToMany(mappedBy = "ancestor", cascade = CascadeType.DETACH)
    @JsonIgnore
    @OrderBy("depth")
    @Getter
    @Setter
    private List<NotesClosure> descendantNCs = new ArrayList<>();

    @JoinTable(name = "notes_closure", joinColumns = {
            @JoinColumn(name = "ancestor_id", referencedColumnName = "id", nullable = false, insertable = false, updatable = false)}, inverseJoinColumns = {
            @JoinColumn(name = "note_id", referencedColumnName = "id", nullable = false, insertable = false, updatable = false)
    })
    @OneToMany(cascade = CascadeType.DETACH)
    @JsonIgnore
    @WhereJoinTable(clause = "depth = 1")
    @OrderBy("sibling_order")
    @Getter
    private final List<Note> children = new ArrayList<>();

    public static Note createNote(User user, NoteContent noteContent, Timestamp currentUTCTimestamp) throws IOException {
        final Note note = new Note();
        note.updateNoteContent(noteContent, user);
        note.setCreatedAtAndUpdatedAt(currentUTCTimestamp);
        note.setUser(user);
        return note;
    }

    @Override
    public String toString() {
        return "Note{" + "id=" + id + ", title='" + noteContent.getTitle() + '\'' + '}';
    }

    public String getShortDescription() {
        return noteContent.getShortDescription();
    }

    @JsonIgnore
    public List<Note> getTargetNotes() {
        return links.stream().map(Link::getTargetNote).collect(toList());
    }

    @JsonIgnore
    public NoteWithPosition jsonNoteWithPosition(User viewer) {
        NoteWithPosition nvb = new NoteWithPosition();
        nvb.setNote(jsonObjectViewedBy(viewer));
        nvb.setNotePosition(jsonNotePosition(viewer));
        return nvb;
    }

    @JsonIgnore
    public NotePositionViewedByUser jsonNotePosition(User viewer) {
        NotePositionViewedByUser nvb = new NotePositionViewedByUser();
        nvb.setNoteId(getId());
        nvb.setTitle(getTitle());
        nvb.setNotebook(notebook);
        nvb.setAncestors(getAncestors());
        nvb.setOwns(viewer != null && viewer.owns(notebook));
        return nvb;
    }

    @JsonIgnore
    public NoteViewedByUser jsonObjectViewedBy(User viewer) {
        NoteViewedByUser nvb = new NoteViewedByUser();
        nvb.setId(getId());
        nvb.setParentId(getParentId());
        nvb.setTitle(getTitle());
        nvb.setShortDescription(getShortDescription());
        nvb.setNotePicture(getNotePicture());
        nvb.setCreatedAt(getCreatedAt());
        nvb.setNoteContent(getNoteContent());
        nvb.setLinks(getAllLinks(viewer));
        nvb.setChildrenIds(children.stream().map(Note::getId).collect(Collectors.toUnmodifiableList()));
        return nvb;
    }

    public Map<Link.LinkType, LinkViewed> getAllLinks(User viewer) {
        return Arrays.stream(Link.LinkType.values())
                .map(type->Map.entry(type, new LinkViewed() {{
                    setDirect(linksOfTypeThroughDirect(List.of(type), viewer).collect(Collectors.toUnmodifiableList()));
                            setReverse(linksOfTypeThroughReverse(type, viewer).collect(Collectors.toUnmodifiableList()));
                        }}))
                .filter(x -> x.getValue().notEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Stream<Link> linksOfTypeThroughDirect(List<Link.LinkType> linkTypes, User viewer) {
        return this.links.stream()
                .filter(l -> l.targetVisibleAsSourceOrTo(viewer))
                .filter(l -> linkTypes.contains(l.getLinkType()));
    }

    public Stream<Link> linksOfTypeThroughReverse(Link.LinkType linkType, User viewer) {
        return refers.stream()
                .filter(l -> l.getLinkType().equals(linkType))
                .filter(l -> l.sourceVisibleAsTargetOrTo(viewer));
    }

    public String getNotePicture() {
        if (noteContent.getUseParentPicture() && getParentNote() != null) {
            return getParentNote().getNotePicture();
        }
        return noteContent.getNotePicture();
    }

    public boolean isHead() {
        return getParentNote() == null;
    }

    private void addAncestors(List<Note> ancestors) {
        int[] counter = {1};
        ancestors.forEach(anc -> {
            NotesClosure notesClosure = new NotesClosure();
            notesClosure.setNote(this);
            notesClosure.setAncestor(anc);
            notesClosure.setDepth(counter[0]);
            getNotesClosures().add(0, notesClosure);
            counter[0] += 1;
        });
    }

    public void setParentNote(Note parentNote) {
        if (parentNote == null) return;
        notebook = parentNote.getNotebook();
        List<Note> ancestorsIncludingMe = parentNote.getAncestorsIncludingMe();
        Collections.reverse(ancestorsIncludingMe);
        addAncestors(ancestorsIncludingMe);
    }

    @JsonIgnore
    public List<Note> getAncestorsIncludingMe() {
        List<Note> ancestors = getAncestors();
        ancestors.add(this);
        return ancestors;
    }

    @JsonIgnore
    public List<Note> getAncestors() {
        return getNotesClosures().stream().map(NotesClosure::getAncestor).collect(toList());
    }

    public void traverseBreadthFirst(Consumer<Note> noteConsumer) {
        descendantNCs.stream().map(NotesClosure::getNote).forEach(noteConsumer);
    }

    @JsonIgnore
    public Note getParentNote() {
        List<Note> ancestors = getAncestors();
        if (ancestors.size() == 0) {
            return null;
        }
        return ancestors.get(ancestors.size() - 1);
    }

    @JsonIgnore
    public List<Note> getSiblings() {
        if (getParentNote() == null) {
            return new ArrayList<>();
        }
        return getParentNote().getChildren();
    }

    public String getTitle() {
        return noteContent.getTitle();
    }

    public void mergeMasterReviewSetting(ReviewSetting reviewSetting) {
        ReviewSetting current = getMasterReviewSetting();
        if (current == null) {
            setMasterReviewSetting(reviewSetting);
        } else {
            BeanUtils.copyProperties(reviewSetting, getMasterReviewSetting());
        }
    }

    public void updateNoteContent(NoteContent noteContent, User user) throws IOException {
        noteContent.fetchUploadedPicture(user);
        mergeNoteContent(noteContent);
    }

    public void mergeNoteContent(NoteContent noteContent) {
        if (noteContent.getUploadPicture() == null) {
            noteContent.setUploadPicture(getNoteContent().getUploadPicture());
        }
        BeanUtils.copyProperties(noteContent, getNoteContent());
    }

    @JsonIgnore
    private Note getFirstChild() {
        return getChildren().stream().findFirst().orElse(null);
    }

    public void updateSiblingOrder(Note relativeToNote, boolean asFirstChildOfNote) {
        Long newSiblingOrder = relativeToNote.theSiblingOrderItTakesToMoveRelativeToMe(asFirstChildOfNote);
        if (newSiblingOrder != null) {
            siblingOrder = newSiblingOrder;
        }
    }

    private Optional<Note> nextSibling() {
        return getSiblings().stream()
                .filter(nc -> nc.siblingOrder > siblingOrder)
                .findFirst();
    }

    private long getSiblingOrderToInsertBehindMe() {
        Optional<Note> nextSiblingNote = nextSibling();
        return nextSiblingNote.map(x -> (siblingOrder + x.siblingOrder) / 2)
                .orElse(siblingOrder + SiblingOrder.MINIMUM_SIBLING_ORDER_INCREMENT);
    }

    private Long getSiblingOrderToBecomeMyFirstChild() {
        Note firstChild = getFirstChild();
        if (firstChild != null) {
            return firstChild.siblingOrder - SiblingOrder.MINIMUM_SIBLING_ORDER_INCREMENT;
        }
        return null;
    }

    private Long theSiblingOrderItTakesToMoveRelativeToMe(boolean asFirstChildOfNote) {
        if (!asFirstChildOfNote) {
            return getSiblingOrderToInsertBehindMe();
        }
        return getSiblingOrderToBecomeMyFirstChild();
    }

    public void buildNotebookForHeadNote(Ownership ownership, User creator) {
        final Notebook notebook = new Notebook();
        notebook.setCreatorEntity(creator);
        notebook.setOwnership(ownership);
        notebook.setHeadNote(this);

        this.notebook = notebook;
    }

    public Integer getParentId() {
        Note parent = getParentNote();
        if (parent == null) return null;
        return parent.id;
    }

    @JsonIgnore
    public Note getGrandAsPossilbe() {
        Note grand = this;
        for(int i = 0; i < 2; i ++)
            if(grand.getParentNote() != null)
                grand = grand.getParentNote();
        return grand;
    }

    public void setCreatedAtAndUpdatedAt(Timestamp currentUTCTimestamp) {
        this.createdAt = currentUTCTimestamp;
        this.getNoteContent().setUpdatedAt(currentUTCTimestamp);
    }
}

