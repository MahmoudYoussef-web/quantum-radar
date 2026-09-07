package com.quradar.common;

import com.quradar.device.DeviceNotFoundException;
import com.quradar.device.InactiveDeviceException;
import com.quradar.driver.DriverNotFoundException;
import com.quradar.rules.RuleNotFoundException;
import com.quradar.vehicle.VehicleNotFoundException;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({RuleNotFoundException.class, DeviceNotFoundException.class,
            DriverNotFoundException.class, VehicleNotFoundException.class})
    public ResponseEntity<Map<String, String>> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(InactiveDeviceException.class)
    public ResponseEntity<Map<String, String>> handleInactiveDevice(InactiveDeviceException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleConflict(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Conflict: duplicate data (a replayed eventId returns 409)"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid request"));
    }
}
