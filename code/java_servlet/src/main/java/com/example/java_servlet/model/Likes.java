/*
 * Album Store API
 * CS6650 Fall 2023
 *
 * OpenAPI spec version: 1.2
 * Contact: i.gorton@northeasern.edu
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.example.java_servlet.model;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * Likes
 */

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaClientCodegen", date = "2023-11-30T18:39:49.279056944Z[GMT]")

public class Likes {
  @SerializedName("likes")
  private String likes = null;

  @SerializedName("dislikes")
  private String dislikes = null;

  public Likes likes(String likes) {
    this.likes = likes;
    return this;
  }

   /**
   * num likes
   * @return likes
  **/
  @Schema(example = "7", description = "num likes")
  public String getLikes() {
    return likes;
  }

  public void setLikes(String likes) {
    this.likes = likes;
  }

  public Likes dislikes(String dislikes) {
    this.dislikes = dislikes;
    return this;
  }

   /**
   * num dislike
   * @return dislikes
  **/
  @Schema(example = "7", description = "num dislike")
  public String getDislikes() {
    return dislikes;
  }

  public void setDislikes(String dislikes) {
    this.dislikes = dislikes;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Likes likes = (Likes) o;
    return Objects.equals(this.likes, likes.likes) &&
        Objects.equals(this.dislikes, likes.dislikes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(likes, dislikes);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Likes {\n");
    
    sb.append("    likes: ").append(toIndentedString(likes)).append("\n");
    sb.append("    dislikes: ").append(toIndentedString(dislikes)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}
