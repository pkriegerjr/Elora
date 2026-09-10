@RestController @RequestMapping("/api/juridico") @RequiredArgsConstructor
public class JuridicoController {
private final JuridicoService s; @postMapping("/analise") public AnaliseJuridica criar(@RequestBody @Valid analiseJuridica a){return s.analisar(a);} @postMapping("/rescisoes") public ProcessoRescisao criar(@RequestBody @Valid ProcessoRescisao r){return s.solicitar(r);} @Getmapping("/painel") public Map<String,Object> painel(){return s.painel();} }
