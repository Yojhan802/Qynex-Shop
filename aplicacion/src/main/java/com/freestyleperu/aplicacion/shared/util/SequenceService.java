package com.freestyleperu.aplicacion.shared.util;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SequenceService {

    private final SequenceRepository sequenceRepository;

    public SequenceService(SequenceRepository sequenceRepository) {
        this.sequenceRepository = sequenceRepository;
    }

    /**
     * Devuelve el siguiente valor formateado como {@code PREFIJO-00001} para
     * la secuencia {@code name}, creándola si es la primera vez que se usa.
     * Requiere ejecutarse dentro de la transacción del llamador para que el
     * bloqueo pesimista cubra toda la operación que consume el número.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public String next(String name, String prefix, int padding) {
        Sequence sequence = sequenceRepository.lockByName(name).orElseGet(() -> crear(name, prefix, padding));
        long value = sequence.getCurrentValue() + 1;
        sequence.setCurrentValue(value);
        return prefix + "-" + String.format("%0" + padding + "d", value);
    }

    private Sequence crear(String name, String prefix, int padding) {
        try {
            Sequence sequence = new Sequence();
            sequence.setName(name);
            sequence.setPrefix(prefix);
            sequence.setCurrentValue(0);
            sequence.setPadding(padding);
            return sequenceRepository.saveAndFlush(sequence);
        } catch (DataIntegrityViolationException alreadyCreatedConcurrently) {
            return sequenceRepository.lockByName(name).orElseThrow();
        }
    }
}
