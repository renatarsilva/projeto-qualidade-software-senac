package br.senac.recursos.auditoria;

import br.senac.recursos.seguranca.Perfil;
import br.senac.recursos.seguranca.RegraDeAcesso;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

/** Só o Responsável e o Administrador consultam o histórico (DP-02). Não há regra de escrita: negado por padrão. */
@Configuration
public class AuditoriaAcessoConfiguracao {

    @Bean
    RegraDeAcesso consultaDeAuditoria() {
        return RegraDeAcesso.soPara(HttpMethod.GET, "/api/reservas/*/auditoria", Perfil.RESPONSAVEL,
                Perfil.ADMINISTRADOR);
    }
}
