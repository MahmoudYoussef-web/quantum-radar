package com.quradar.fine;

import com.quradar.violation.ViolationResponse;
import java.util.List;

public record FineResponse(String plateNumber, int totalAmount, List<ViolationResponse> violations) {

    public static FineResponse from(Fine fine) {
        return new FineResponse(fine.getPlateNumber(), fine.getTotalAmount(),
                fine.getViolations().stream().map(ViolationResponse::from).toList());
    }
}
