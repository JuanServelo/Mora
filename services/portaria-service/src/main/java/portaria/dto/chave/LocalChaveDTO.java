package portaria.dto.chave;

import portaria.model.enums.TipoLocal;

public record LocalChaveDTO(String id, String nome, TipoLocal tipo) {}
