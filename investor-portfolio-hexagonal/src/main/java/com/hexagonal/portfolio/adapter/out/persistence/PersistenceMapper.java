package com.hexagonal.portfolio.adapter.out.persistence;

import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Translates JPA entities into their domain counterparts.
 *
 * <p>Each domain model is a field-for-field copy of its entity minus the persistence
 * annotations, so a property copy is a faithful translation. Keeping this in one place
 * means the application layer never sees a managed entity.
 */
final class PersistenceMapper {

    private PersistenceMapper() {
    }

    static <T> T map(Object source, Class<T> targetType) {
        if (source == null) {
            return null;
        }
        try {
            T target = targetType.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);
            return target;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Cannot instantiate " + targetType.getName(), ex);
        }
    }

    static <T> List<T> mapList(List<?> source, Class<T> targetType) {
        if (source == null) {
            return new ArrayList<>();
        }
        List<T> out = new ArrayList<>(source.size());
        for (Object item : source) {
            out.add(map(item, targetType));
        }
        return out;
    }
}
