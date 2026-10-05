package cl.casero.migration.service.dto;

import java.util.List;

import cl.casero.migration.util.CustomerScoreSummary.CycleScore;

public record CustomerScorePresentation(double score, String explanation, List<CycleScore> cycles) {}
