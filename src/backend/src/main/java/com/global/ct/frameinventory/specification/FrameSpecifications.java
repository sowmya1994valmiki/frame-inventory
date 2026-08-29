package com.global.ct.frameinventory.specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.global.ct.frameinventory.dto.FrameSearchCriteria;
import com.global.ct.frameinventory.entity.Frame;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

public final class FrameSpecifications {

    private FrameSpecifications() {
    }

    public static Specification<Frame> matching(FrameSearchCriteria criteria) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.query() != null) {
                String pattern = "%" + escapeLike(criteria.query().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(builder.or(
                    like(builder, root.get("frameId"), pattern),
                    like(builder, root.get("location").get("address"), pattern),
                    like(builder, root.get("location").get("town"), pattern),
                    like(builder, root.get("site").get("station"), pattern),
                    like(builder, root.get("site").get("airport"), pattern),
                    like(builder, root.get("site").get("siteNumber"), pattern)
                ));
            }

            addExact(predicates, builder, root.get("status"), criteria.status());
            addExact(predicates, builder, root.get("mediaType"), criteria.mediaType());
            addExact(predicates, builder, root.get("environment"), criteria.environment());
            addExact(predicates, builder, root.get("format"), criteria.format());
            addExact(predicates, builder, root.get("location").get("region"), criteria.region());

            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Predicate like(CriteriaBuilder builder, Path<?> path, String pattern) {
        return builder.like(lower(builder, path), pattern, '\\');
    }

    private static void addExact(
        List<Predicate> predicates,
        CriteriaBuilder builder,
        Path<?> path,
        String value
    ) {
        if (value != null) {
            predicates.add(builder.equal(lower(builder, path), value.toLowerCase(Locale.ROOT)));
        }
    }

    private static Expression<String> lower(CriteriaBuilder builder, Path<?> path) {
        return builder.lower(path.as(String.class));
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
