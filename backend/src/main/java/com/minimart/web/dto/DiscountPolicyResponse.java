package com.minimart.web.dto;

import com.minimart.domain.DiscountPolicies;

public record DiscountPolicyResponse(
    String code,
    String name,
    String description
) {
    public static DiscountPolicyResponse from(DiscountPolicies.PolicyDescriptor descriptor) {
        return new DiscountPolicyResponse(descriptor.code(), descriptor.name(), descriptor.description());
    }
}
