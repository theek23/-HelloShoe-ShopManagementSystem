package lk.ijse.gdse66.helloshoes.backend.dto;

import lk.ijse.gdse66.helloshoes.backend.dto.basic.EmployeeBasicDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Theekshana De Silva,
 * @Runtime version: 11.0.11+9-b1341.60amd64
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    private String email;
    private String password;

    private EmployeeBasicDTO employee;
}
