package fr.diginamic.hubevenementiel.dtos.dashboard;

/**
 * One indicator of the dashboard.
 *
 * @param code  stable identifier, usable by the front to pick an icon or a link
 * @param label French label displayed under the value
 * @param value the computed value
 */
public record KpiResponseDto(String code, String label, long value) {
}
