package com.marin.mas_que_amigos.dto;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The teams of one group, chosen by hand when forming the groups.
 */
@Getter
@Setter
@NoArgsConstructor
public class GroupCompositionDTO {

    @NotNull(message = "Cada grupo indicado debe traer sus equipos.")
    private List<Long> teamIds;
}
