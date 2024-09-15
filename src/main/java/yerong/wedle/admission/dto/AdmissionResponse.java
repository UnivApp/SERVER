package yerong.wedle.admission.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AdmissionResponse {
    private String universityName;
    private String universityLocation;
    private String admissionType;
    private int rank;
    private double percentile;
    private boolean isFavorite;

}
