package com.vfu.chatbot.service;

import com.vfu.chatbot.service.domain.PropertyResponse;
import com.vfu.chatbot.service.domain.ReservationResponse;
import org.springframework.stereotype.Component;

/**
 * Single place to format StreamX reservation/property payloads into concise fact strings
 * for LLM context, tool responses, and session cache.
 */
@Component
public class StreamXFactsFormatter {

    public String reservationFacts(ReservationResponse r) {
        return String.format(
                "RESERVATION_FACTS: confirmation_id=%s; reservation_id=%s; guest_name=%s %s; email=%s; mobile_phone=%s; "
                        + "check_in_date=%s; check_out_date=%s; nights=%s; status_code=%s; unit_id=%s; unit_name=%s; unit_code=%s; "
                        + "location_name=%s; condo_type_name=%s; max_occupants=%s; occupants_small=%s; pets=%s; "
                        + "check_in_time=%s; check_out_time=%s; price_total=%s; amount_paid=%s; balance_due=%s.",
                safe(r.getConfirmationId()),
                safe(r.getId()),
                safe(r.getFirstName()),
                safe(r.getLastName()),
                safe(r.getEmail()),
                safe(r.getMobilePhone()),
                safe(r.getStartDate()),
                safe(r.getEndDate()),
                safe(r.getDaysNumber()),
                safe(r.getStatusCode()),
                safe(r.getUnitId()),
                safe(r.getUnitName()),
                safe(r.getUnitCode()),
                safe(r.getLocationName()),
                safe(r.getCondoTypeName()),
                safe(r.getOccupants()),
                safe(r.getOccupantsSmall()),
                safe(r.getPets()),
                safe(r.getCheckInTime()),
                safe(r.getCheckOutTime()),
                safe(r.getPriceTotal()),
                safe(r.getPricePaidsum()),
                safe(r.getPriceBalance())
        );
    }

    public String propertyFacts(PropertyResponse p) {
        return String.format(
                "PROPERTY_FACTS: property_id=%s; unit_name=%s; unit_code=%s; condo_type_name=%s; address=%s; city=%s; state=%s; zip=%s; "
                        + "latitude=%s; longitude=%s; max_adults=%s; max_occupants=%s; max_pets=%s; bedrooms=%s; bathrooms=%s; "
                        + "wifi_key=%s; parking_info=%s; gate_code=%s; driving_directions=%s.",
                safe(p.getId()),
                safe(p.getName()),
                safe(p.getUnitCode()),
                safe(p.getCondoTypeName()),
                safe(p.getAddress()),
                safe(p.getCity()),
                safe(p.getStateName()),
                safe(p.getZip()),
                safe(p.getLatitude()),
                safe(p.getLongitude()),
                safe(p.getMaxAdults()),
                safe(p.getMaxOccupants()),
                safe(p.getMaxPets()),
                safe(p.getBedroomsNumber()),
                safe(p.getBathroomsNumber()),
                safe(p.getWifiSecurityKey()),
                safe(p.getVariableParkingSpaces()),
                safe(p.getVariableGateCode()),
                safe(p.getVariableDrivingDirections())
        );
    }

    private static String safe(Object value) {
        if (value == null) {
            return "UNKNOWN";
        }
        String normalized = String.valueOf(value).trim();
        return normalized.isEmpty() ? "UNKNOWN" : normalized;
    }
}
