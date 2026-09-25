package com.easyprufung.backend.Project.DTO;

import java.util.List;

public class ProjectValidation {

    private String problemSolved;
    private Scores scores;
    private Description description;
    private List<CompetitorAnalysis> competitorAnalysis;

    // Getters and Setters
    public String getProblemSolved() {
        return problemSolved;
    }

    public void setProblemSolved(String problemSolved) {
        this.problemSolved = problemSolved;
    }

    public Scores getScores() {
        return scores;
    }

    public void setScores(Scores scores) {
        this.scores = scores;
    }

    public Description getDescription() {
        return description;
    }

    public void setDescription(Description description) {
        this.description = description;
    }

    public List<CompetitorAnalysis> getCompetitorAnalysis() {
        return competitorAnalysis;
    }

    public void setCompetitorAnalysis(List<CompetitorAnalysis> competitorAnalysis) {
        this.competitorAnalysis = competitorAnalysis;
    }

    // Nested class for Scores
    public static class Scores {
        private MarketDemand marketDemand;
        private Feasibility feasibility;
        private Scalability scalability;
        private RevenuePotential revenuePotential;
        private Risk risk;

        // Getters and Setters
        public MarketDemand getMarketDemand() {
            return marketDemand;
        }

        public void setMarketDemand(MarketDemand marketDemand) {
            this.marketDemand = marketDemand;
        }

        public Feasibility getFeasibility() {
            return feasibility;
        }

        public void setFeasibility(Feasibility feasibility) {
            this.feasibility = feasibility;
        }

        public Scalability getScalability() {
            return scalability;
        }

        public void setScalability(Scalability scalability) {
            this.scalability = scalability;
        }

        public RevenuePotential getRevenuePotential() {
            return revenuePotential;
        }

        public void setRevenuePotential(RevenuePotential revenuePotential) {
            this.revenuePotential = revenuePotential;
        }

        public Risk getRisk() {
            return risk;
        }

        public void setRisk(Risk risk) {
            this.risk = risk;
        }
    }

    // Nested class for MarketDemand
    public static class MarketDemand {
        private int value;
        private String feedback;

        // Getters and Setters
        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public String getFeedback() {
            return feedback;
        }

        public void setFeedback(String feedback) {
            this.feedback = feedback;
        }
    }

    // Nested class for Feasibility
    public static class Feasibility {
        private int value;
        private String feedback;

        // Getters and Setters
        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public String getFeedback() {
            return feedback;
        }

        public void setFeedback(String feedback) {
            this.feedback = feedback;
        }
    }

    // Nested class for Scalability
    public static class Scalability {
        private int value;
        private String feedback;

        // Getters and Setters
        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public String getFeedback() {
            return feedback;
        }

        public void setFeedback(String feedback) {
            this.feedback = feedback;
        }
    }

    // Nested class for RevenuePotential
    public static class RevenuePotential {
        private int value;
        private String feedback;

        // Getters and Setters
        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public String getFeedback() {
            return feedback;
        }

        public void setFeedback(String feedback) {
            this.feedback = feedback;
        }
    }

    // Nested class for Risk
    public static class Risk {
        private int value;
        private String feedback;

        // Getters and Setters
        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public String getFeedback() {
            return feedback;
        }

        public void setFeedback(String feedback) {
            this.feedback = feedback;
        }
    }

    // Nested class for Description
    public static class Description {
        private String shortDescription;
        private String longDescription;
        private String logoDescription;
        private String landingPageDescription;
        // Getters and Setters
        public String getShortDescription() {
            return shortDescription;
        }

        public void setShortDescription(String shortDescription) {
            this.shortDescription = shortDescription;
        }

        public String getLongDescription() {
            return longDescription;
        }

        public void setLongDescription(String longDescription) {
            this.longDescription = longDescription;
        }

        public String getLogoDescription() {
            return logoDescription;
        }

        public void setLogoDescription(String logoDescription) {
            this.logoDescription = logoDescription;
        }

        public String getLandingPageDescription() {
            return landingPageDescription;
        }

        public void setLandingPageDescription(String landingPageDescription) {
            this.landingPageDescription = landingPageDescription;
        }
    }

    // Nested class for CompetitorAnalysis
    public static class CompetitorAnalysis {
        private String name;
        private String url;
        private String overview;
        private String strengths;
        private String weaknesses;

        // Getters and Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getOverview() {
            return overview;
        }

        public void setOverview(String overview) {
            this.overview = overview;
        }

        public String getStrengths() {
            return strengths;
        }

        public void setStrengths(String strengths) {
            this.strengths = strengths;
        }

        public String getWeaknesses() {
            return weaknesses;
        }

        public void setWeaknesses(String weaknesses) {
            this.weaknesses = weaknesses;
        }
    }
}
