package com.finsphere.service;

import com.finsphere.dto.AuctionUpdateRequest;
import com.finsphere.entity.ChitMonthlyCycle;
import com.finsphere.repository.ChitMonthlyCycleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChitCycleService {

    private final ChitMonthlyCycleRepository cycleRepository;

    @Transactional
    public ChitMonthlyCycle updateAuctionResults(AuctionUpdateRequest request) {
        // 1. Find the specific cycle for the plan and month
        ChitMonthlyCycle cycle = cycleRepository.findByPlanIdAndMonthlyCount(
                        request.getPlanId(), request.getMonthlyCount())
                .orElseThrow(() -> new RuntimeException("Monthly cycle not found for this plan/month"));

        // 2. Security Check: Don't allow updating a cycle that is already closed
        if (Boolean.TRUE.equals(cycle.getIsCycleClosed())) {
            throw new RuntimeException("This monthly cycle is already closed and cannot be modified.");
        }

        // 3. Update with manual entries
        cycle.setAuctionWinnerBid(request.getBidAmount());
        cycle.setMonthlyPayableAmount(request.getPayableAmount());
        cycle.setWinnerUserId(request.getWinnerUserId());

        // 4. Mark as closed once the auction details are finalized
        cycle.setIsCycleClosed(true);

        return cycleRepository.save(cycle);
    }
}
