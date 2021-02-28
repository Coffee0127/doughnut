package com.odde.doughnut.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "note")
public class Note {
  @Id @Getter @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
  @NotNull
  @Size(min=1, max=100)
  @Getter @Setter private String title;
  @Getter @Setter private String description;
  @Getter @Setter private String picture;
  @Getter @Setter private String url;
  @Column(name="url_is_video")
  @Getter @Setter private Boolean urlIsVideo = false;
  @Column(name="sibling_order")
  @Getter @Setter private Long siblingOrder = getGoodEnoughOrderNumber();


  //
  // SiblingOrder is used to decide the order among the notes under the same parent.
  // In a real life situation, using millisecond * 1000 is good enough for ordering and
  // also leave enough space for inserting items in between.
  // However, in extreme cases like unit testing or importing batch of notes, there is
  // still a chance of duplicated order number.
  // Since this will likely to happy within the same java running instance, a simple
  // static variable `localLastOrderNumberForGoodEnoughSiblingOrder` is introduced to
  // detect and remove the duplicates.
  //
  static long localLastOrderNumberForGoodEnoughSiblingOrder;
  private static Long getGoodEnoughOrderNumber() {
    long newNumber = System.currentTimeMillis() * 1000;
    if (newNumber <= localLastOrderNumberForGoodEnoughSiblingOrder) {
      localLastOrderNumberForGoodEnoughSiblingOrder += 1000;
    }
    else {
      localLastOrderNumberForGoodEnoughSiblingOrder = newNumber;
    }
    return localLastOrderNumberForGoodEnoughSiblingOrder;
  }

  @Override
  public String toString() {
    return "Note{" +
            "id=" + id +
            ", title='" + title + '\'' +
            '}';
  }

  @ManyToOne
  @JoinColumn(name = "parent_id")
  @JsonIgnore
  @Getter @Setter private Note parentNote;

  @OneToMany(mappedBy = "parentNote", cascade = CascadeType.ALL, orphanRemoval = true)
  @JsonIgnore
  @Getter private List<Note> children = new ArrayList<>();

  @ManyToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "user_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter @Setter private User user;

  @Column(name="created_datetime")
  @Getter @Setter private Date createdDatetime;

  @Column(name="updated_datetime")
  @Getter @Setter private Date updatedDatetime;

  @JoinTable(name = "link", joinColumns = {
          @JoinColumn(name = "source_id", referencedColumnName = "id", nullable = false)}, inverseJoinColumns = {
            @JoinColumn(name = "target_id", referencedColumnName = "id", nullable = false)
          })
  @ManyToMany
  @JsonIgnoreProperties("targetNotes")
  @Getter @Setter private List<Note> targetNotes = new ArrayList<>();

  @Transient
  @Getter @Setter private String testingLinkTo;

  public void linkToNote(Note targetNote) {
    this.targetNotes.add(targetNote);
  }

  public void addChild(Note note) {
      note.setParentNote(this);
      getChildren().add(note);
  }
}


