
package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/links")
class RestLinkController {
  private final ModelFactoryService modelFactoryService;
  private final CurrentUserFetcher currentUserFetcher;

  public RestLinkController(ModelFactoryService modelFactoryService, CurrentUserFetcher currentUserFetcher) {
    this.modelFactoryService = modelFactoryService;
    this.currentUserFetcher = currentUserFetcher;
  }

  class LinkStatistics {
    @Getter
    @Setter
    private ReviewPoint reviewPoint;
    @Getter
    @Setter
    private Link link;

  }

  @GetMapping("/{link}/statistics")
  public LinkStatistics statistics(@PathVariable("link") Link link) throws NoAccessRightException {
    final UserModel user = currentUserFetcher.getUser();
    user.getAuthorization().assertAuthorization(link);
    LinkStatistics statistics = new LinkStatistics();
    statistics.setReviewPoint(user.getReviewPointFor(link));
    statistics.setLink(link);
    return statistics;
  }
}