---
name: android-uiux-redesign-review
description: 'Audit and redesign Android app UI/UX with a production-grade process covering heuristic review, visual direction, interaction quality, accessibility, implementation guidance, and acceptance checks including gesture-navigation bottom inset issues.'
argument-hint: 'Provide app purpose, target users, current screens, design constraints, and priority flows to redesign.'
user-invocable: true
disable-model-invocation: false
---

# Android UI/UX Redesign Review

Use this skill to review an existing Android app interface and produce a redesign plan that is distinctive, practical, and implementation-ready.

## When To Use
- The app feels dated, inconsistent, or visually generic.
- Navigation, hierarchy, or task completion is confusing.
- The team needs a high-quality redesign plan before coding.
- You need explicit checks for Android system insets and gesture navigation behavior.

## Inputs
Provide as many as available:
- Product context: goal, audience, tone, platform constraints.
- Existing screens: screenshots, Compose/XML files, user flows.
- Technical constraints: minSdk, design system, performance and accessibility requirements.
- Priority journeys: top 3 to 5 actions users must complete quickly.

## Procedure
1. Define redesign objective and measurable outcomes.
2. Map current primary journeys and friction points.
3. Perform heuristic review across usability, clarity, feedback, and error handling.
4. Audit visual language: typography, spacing, color, elevation, iconography, and motion.
5. Audit interaction quality: touch targets, gesture conflicts, loading states, transitions, and empty states.
6. Run Android platform fit checks, including edge-to-edge behavior and system bar insets.
7. Select one bold but coherent visual direction aligned to product tone.
8. Produce redesign recommendations per screen with rationale and implementation notes.
9. Define acceptance criteria and regression checks for QA.

## Decision Points
- If information is incomplete:
  Request missing context before final recommendations.
- If the app is inconsistent but functional:
  Prioritize design system tokens and component consistency first.
- If flows are broken:
  Prioritize information architecture and navigation before styling polish.
- If the redesign conflicts with technical constraints:
  Propose two options: ideal direction and constrained fallback.

## Review Checklist
- Information hierarchy is clear within 3 seconds per screen.
- Primary actions are visually dominant and reachable with one hand.
- Typography scale is intentional and readable at system font scaling.
- Color contrast supports accessibility and state clarity.
- Spacing rhythm is consistent across components.
- Motion supports comprehension, not decoration only.
- Empty, loading, and error states are designed and actionable.
- Navigation labels and back behavior are predictable.
- Edge-to-edge content correctly applies insets.
- No white strip, blank band, or clipping appears near the bottom when using gesture navigation.

## Android Inset And Gesture Navigation Requirements
- Enable edge-to-edge drawing intentionally for the activity.
- Apply WindowInsets for top and bottom bars on root containers and scrolling content.
- Verify bottom components (bottom nav, FAB, CTA bars, sheets) respect navigation bar/gesture insets.
- Confirm backgrounds extend behind system bars where intended.
- Validate behavior on 3-button and gesture navigation modes.
- Test portrait and landscape, including keyboard open/close transitions.

## Output Format
Return all sections:
1. Redesign intent and chosen visual direction.
2. Top UX issues ranked by severity.
3. Screen-by-screen redesign actions.
4. Android implementation guidance (Compose/XML specific where possible).
5. Acceptance criteria and QA test matrix.

## Completion Criteria
- Recommendations cover at least one end-to-end primary journey.
- Each proposed change is tied to a UX problem and expected impact.
- Accessibility and platform-fit checks are explicit.
- Gesture-navigation bottom-area issue is explicitly addressed with verification steps.

## Example Prompts
- Review this Android app UI and produce a redesign plan focused on onboarding and transaction entry.
- Audit my Compose screens for UX and visual quality, then propose a bold redesign direction with implementation steps.
- Redesign this app to feel premium and modern while preserving current feature scope; include inset and gesture-nav QA checks.