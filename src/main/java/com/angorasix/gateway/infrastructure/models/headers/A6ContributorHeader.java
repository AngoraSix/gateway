package com.angorasix.gateway.infrastructure.models.headers;

import java.util.Map;

/**
 * <p>
 * Header that will be sent downstream with Contributor information.
 * </p>
 *
 * @author rozagerardo
 */
public class A6ContributorHeader {

  private final String contributorId;
  private final Map<String, String> attributes;
  private boolean projectAdmin;

  public A6ContributorHeader(final String contributorId,
      final Map<String, String> attributes) {
    this.contributorId = contributorId;
    this.attributes = attributes;
  }

  public String getContributorId() {
    return contributorId;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  public boolean isProjectAdmin() {
    return projectAdmin;
  }

  public void setProjectAdmin(final boolean projectAdmin) {
    this.projectAdmin = projectAdmin;
  }
}
