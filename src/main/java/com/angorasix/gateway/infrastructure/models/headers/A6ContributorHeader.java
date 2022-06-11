package com.angorasix.gateway.infrastructure.models.headers;

import java.util.Map;

/**
 * <p>
 * </p>
 *
 * @author rozagerardo
 */
public class A6ContributorHeader {

  private final String contributorId;
  private final Map<String, String> attributes;
  private boolean isProjectAdmin = false;

  public A6ContributorHeader(String contributorId,
      Map<String, String> attributes) {
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
    return isProjectAdmin;
  }

  public void setProjectAdmin(boolean projectAdmin) {
    isProjectAdmin = projectAdmin;
  }
}
