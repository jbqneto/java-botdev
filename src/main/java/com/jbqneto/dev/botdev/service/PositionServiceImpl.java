package com.jbqneto.dev.botdev.service;

import com.jbqneto.dev.botdev.dto.ActivePositionDto;
import lombok.extern.slf4j.Slf4j; // Added
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class PositionServiceImpl implements PositionService {

    private final Map<String, ActivePositionDto> activePositions = new ConcurrentHashMap<>();

    @Override
    public Optional<ActivePositionDto> getPosition(String symbol) {
        return Optional.ofNullable(activePositions.get(symbol));
    }

    @Override
    public void updatePosition(ActivePositionDto position) {
        if (position == null || position.getSymbol() == null || position.getSymbol().isEmpty()) {
            log.warn("Attempted to update position with null or invalid symbol."); // logger to log
            return;
        }
        activePositions.put(position.getSymbol(), position);
        log.info("Position updated/added: {}", position); // logger to log
    }

    @Override
    public void removePosition(String symbol) {
        if (symbol == null || symbol.isEmpty()) {
            log.warn("Attempted to remove position with null or invalid symbol."); // logger to log
            return;
        }
        ActivePositionDto removedPosition = activePositions.remove(symbol);
        if (removedPosition != null) {
            log.info("Position removed for symbol {}: {}", symbol, removedPosition); // logger to log
        } else {
            log.info("No active position found to remove for symbol: {}", symbol); // logger to log
        }
    }

    @Override
    public Set<String> getAllActiveSymbols() {
        return activePositions.keySet();
    }

    @Override
    public boolean hasActivePosition(String symbol) {
        return activePositions.containsKey(symbol);
    }
}
