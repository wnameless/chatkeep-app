package me.moonote.app.chatkeep.dto;

/**
 * Enum representing the type of reference in a conversation archive.
 *
 * References are classified by a simple binary distinction: whether they have a URL or not.
 */
public enum ReferenceType {

  /**
   * External link with a URL (e.g., documentation, GitHub repositories, articles).
   *
   * These references are clickable and citable.
   */
  EXTERNAL_LINK,

  /**
   * Descriptive reference without a URL.
   *
   * Includes concepts (e.g., CAP Theorem, Design Patterns), contextual information (e.g., version
   * numbers, dates), and other general references mentioned during the conversation.
   *
   * The description itself conveys the nature of the reference - no need for further
   * classification.
   */
  DESCRIPTIVE
}
