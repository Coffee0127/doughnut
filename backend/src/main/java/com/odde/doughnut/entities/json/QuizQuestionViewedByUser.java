package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.PictureWithMask;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.quizFacotries.QuizQuestionPresenter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;

public class QuizQuestionViewedByUser {

  public QuizQuestion quizQuestion;

  @Getter public QuizQuestion.QuestionType questionType;

  @Getter public String description;

  @Getter public String mainTopic;

  @Getter public Map<Link.LinkType, LinkViewed> hintLinks;

  @Getter public List<Integer> viceReviewPointIdList;

  public List<Note> scope;

  @Getter public List<Option> options;

  public Optional<PictureWithMask> pictureWithMask;

  public QuizQuestionViewedByUser(
      QuizQuestion quizQuestion, ModelFactoryService modelFactoryService) {
    QuizQuestionPresenter presenter = quizQuestion.buildPresenter();
    this.quizQuestion = quizQuestion;
    questionType = quizQuestion.getQuestionType();
    description = presenter.instruction();
    mainTopic = presenter.mainTopic();
    hintLinks = presenter.hintLinks();
    pictureWithMask = presenter.pictureWithMask();
    options =
        presenter.optionCreator().getOptions(modelFactoryService, quizQuestion.getOptionNoteIds());
    viceReviewPointIdList = quizQuestion.getViceReviewPointIdList();
    scope = List.of(quizQuestion.getReviewPoint().getSourceNote().getNotebook().getHeadNote());
  }

  public static class Option {
    @Getter private Integer noteId;
    @Getter private boolean isPicture = false;
    @Getter private String display;
    @Getter private Optional<PictureWithMask> pictureWithMask;

    private Option() {}
  }

  public interface OptionCreator {

    default List<Option> getOptions(ModelFactoryService modelFactoryService, String optionNoteIds) {
      Stream<Note> noteStream =
          modelFactoryService.noteRepository.findAllByIds(optionNoteIds.split(","));
      return noteStream.map(this::optionFromNote).toList();
    }

    Option optionFromNote(Note note);
  }

  public static class TitleOptionCreator implements OptionCreator {
    @Override
    public Option optionFromNote(Note note) {
      Option option = new Option();
      option.noteId = note.getId();
      option.display = note.getTitle();
      return option;
    }
  }

  public static class PictureOptionCreator implements OptionCreator {
    @Override
    public Option optionFromNote(Note note) {
      Option option = new Option();
      option.noteId = note.getId();
      option.display = note.getTitle();
      option.pictureWithMask = note.getPictureWithMask();
      option.isPicture = true;
      return option;
    }
  }

  public static class ClozeLinkOptionCreator implements OptionCreator {
    @Override
    public List<Option> getOptions(ModelFactoryService modelFactoryService, String optionNoteIds) {
      Stream<Link> noteStream =
          modelFactoryService.linkRepository.findAllByIds(optionNoteIds.split(","));
      return noteStream.map(this::optionFromLink).toList();
    }

    public Option optionFromLink(Link link) {
      Option option = new Option();
      option.noteId = link.getSourceNote().getId();
      option.display = link.getClozeSource();
      return option;
    }

    @Override
    public Option optionFromNote(Note note) {
      return null;
    }
  }
}
