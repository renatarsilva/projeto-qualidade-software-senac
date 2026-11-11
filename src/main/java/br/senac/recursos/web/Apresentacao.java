package br.senac.recursos.web;

import br.senac.recursos.reserva.EstadoDaReserva;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Formatação para as telas (bean "ap"): horário de Brasília, nomes dos estados e cores dos selos. */
@Component("ap")
public class Apresentacao {

    private static final DateTimeFormatter DATA_E_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(Fuso.BRASILIA);
    private static final DateTimeFormatter CAMPO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
            .withZone(Fuso.BRASILIA);
    private static final Locale PT_BR = Locale.of("pt", "BR");

    public String data(Instant instante) {
        return instante == null ? "" : DATA_E_HORA.format(instante);
    }

    /** O valor de um campo datetime-local. */
    public String campo(Instant instante) {
        return instante == null ? "" : CAMPO.format(instante);
    }

    public String horas(double horas) {
        return String.format(PT_BR, "%.2f", horas);
    }

    public String estado(EstadoDaReserva estado) {
        if (estado == null) {
            return "Criada";
        }
        return switch (estado) {
            case SOLICITADA -> "Solicitada";
            case APROVADA -> "Aprovada";
            case EM_USO -> "Em uso";
            case CONCLUIDA -> "Concluída";
            case REJEITADA -> "Rejeitada";
            case CANCELADA -> "Cancelada";
            case NAO_COMPARECEU -> "Não compareceu";
        };
    }

    public String selo(EstadoDaReserva estado) {
        if (estado == null) {
            return "text-bg-secondary";
        }
        return switch (estado) {
            case SOLICITADA -> "text-bg-warning";
            case APROVADA, EM_USO -> "text-bg-success";
            case CONCLUIDA -> "text-bg-primary";
            case REJEITADA, CANCELADA, NAO_COMPARECEU -> "text-bg-secondary";
        };
    }

    public String tipo(String tipo) {
        return switch (tipo) {
            case "SALA" -> "Sala";
            case "PROFESSOR" -> "Professor";
            case "MATERIAL" -> "Material";
            default -> tipo;
        };
    }

    public String perfil(String perfil) {
        return switch (perfil) {
            case "SOLICITANTE" -> "Solicitante";
            case "RESPONSAVEL" -> "Responsável";
            case "ADMINISTRADOR" -> "Administrador";
            default -> perfil;
        };
    }
}
