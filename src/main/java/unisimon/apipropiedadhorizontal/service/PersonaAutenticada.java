package unisimon.apipropiedadhorizontal.service;

import unisimon.apipropiedadhorizontal.domain.model.Persona;

/** Persona asociada a la cuenta del token. Los casos propios parten de aqui, nunca de un id enviado por el cliente. */
public interface PersonaAutenticada {

    /** 422 si la cuenta no tiene persona asociada. */
    Long id();

    Persona entidad();
}
