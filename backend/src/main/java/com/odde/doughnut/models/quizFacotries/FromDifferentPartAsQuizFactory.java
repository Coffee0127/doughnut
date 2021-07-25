package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.json.LinkViewed;
import com.odde.doughnut.models.UserModel;

import java.util.*;
import java.util.stream.Collectors;

public class FromDifferentPartAsQuizFactory implements QuizQuestionFactory {
    protected final QuizQuestionServant servant;
    protected final ReviewPoint reviewPoint;
    protected final Link link;
    private Link cachedAnswerLink = null;
    private List<Note> cachedFillingOptions = null;
    private Optional<Link> categoryLink = null;

    public FromDifferentPartAsQuizFactory(QuizQuestionServant servant, ReviewPoint reviewPoint) {
        this.servant = servant;
        this.reviewPoint = reviewPoint;
        this.link = reviewPoint.getLink();
    }

    protected Link getAnswerLink() {
        if (cachedAnswerLink == null) {
            cachedAnswerLink = getCategoryLink()
                    .map(lk -> servant.randomizer.chooseOneRandomly(link.getRemoteCousinOfDifferentCategory(lk, reviewPoint.getUser())))
                    .orElse(null);
        }
        return cachedAnswerLink;
    }

    @Override
    public boolean isValidQuestion() {
        return generateAnswerNote() != null && generateFillingOptions().size() > 0;
    }

    @Override
    public List<ReviewPoint> getViceReviewPoints() {
        UserModel userModel = servant.modelFactoryService.toUserModel(reviewPoint.getUser());
        return getCategoryLink().map(userModel::getReviewPointFor).map(List::of).orElse(Collections.emptyList());
    }

    private Optional<Link> getCategoryLink() {
        if (categoryLink == null) {
            categoryLink = servant.chooseOneCategoryLink(reviewPoint.getUser(), link);
        }
        return categoryLink;
    }

    @Override
    public List<Note> generateFillingOptions() {
        if (cachedFillingOptions == null) {
            List<Link> cousinLinks = link.getCousinLinksOfSameLinkType(reviewPoint.getUser());
            cachedFillingOptions = servant.randomizer.randomlyChoose(5, cousinLinks).stream()
                    .map(Link::getSourceNote).collect(Collectors.toList());
        }
        return cachedFillingOptions;
    }

    @Override
    public String generateInstruction() {
        return "<p>Which one <mark>" + link.getLinkTypeLabel() + "</mark> a <em>DIFFERENT</em> <mark>" + getCategoryLink().map(lk->lk.getTargetNote().getTitle()).orElse("") + "</mark> than:";
    }

    @Override
    public String generateMainTopic() {
        return link.getSourceNote().getTitle();
    }

    @Override
    public Note generateAnswerNote() {
        if (getAnswerLink() == null) return null;
        return getAnswerLink().getSourceNote();
    }

    @Override
    public List<QuizQuestion.Option> toQuestionOptions(List<Note> notes) {
        return servant.toTitleOptions(notes);
    }

    @Override
    public Map<Link.LinkType, LinkViewed> generateHintLinks() {
        return null;
    }
}