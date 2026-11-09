package com.threadflow.production;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class ProductionDelayDtos {
    private ProductionDelayDtos() {}

    public enum RiskStatus { ON_TRACK, AT_RISK, DELAYED, COMPLETED }

    public record OrderDelayRiskResponse(
            Long orderId,
            String poNumber,
            String buyerName,
            String styleName,
            LocalDate exFactoryDate,
            LocalDate calculatedForDate,
            String finalMilestoneName,
            int totalQuantity,
            long reportedCompletedQuantity,
            int completedQuantity,
            int remainingQuantity,
            long elapsedProductionDays,
            long remainingProductionDays,
            BigDecimal requiredDailyRate,
            BigDecimal actualDailyRate,
            BigDecimal estimatedCompletionDays,
            LocalDate estimatedCompletionDate,
            BigDecimal productionShortfall,
            BigDecimal completionPercent,
            RiskStatus riskStatus,
            List<String> reasons
    ) {}

    public record ProductionSummaryResponse(
            LocalDate calculatedForDate,
            int totalOrders,
            int onTrack,
            int atRisk,
            int delayed,
            int completed
    ) {}
}