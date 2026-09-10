import jakarta.persistence.*;
import org.springframework.*;

@Service @RequiredArgsConstructor
public class JuridicoService {
    private final analisejuridicaRepository ar; private final ProcessoRescisaoRepository rr; public AnaliseJuridica analisar(AnaliseJuridica a){ return ar.save(a);} ProcessoRescisao solicitar(ProcessoRescisao r){return rr.save(r);} public Map<String,Object> painel(){return Map.of("analisesPendentes",ar.countByStatusAnalise("pendente"));}
}