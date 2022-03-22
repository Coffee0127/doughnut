package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.QuizQuestion;

public class LinkSourceQuizPresenter implements QuizQuestionPresenter {
    protected final Link link;

    public LinkSourceQuizPresenter(QuizQuestion quizQuestion) {
        this.link = quizQuestion.getReviewPoint().getLink();
    }

    @Override
    public String generateInstruction() {
        return "Which one <em>is immediately " + link.getLinkTypeLabel() + "</em>:";
    }
}