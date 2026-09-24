@RestController @RequestMapping("/conhecimento") @RequiredArgsConstructor
public class ConhecimentoController {
  private final ConhecimentoService service;
  @GetMapping("/artigos") public ApiResponse<List<ArtigoResponse>> listar(){ return ApiResponse.ok(service.listarPublicados()); }
  @GetMapping("/artigos/{id}") public ApiResponse<ArtigoResponse> get(@PathVariable Integer id){ return null; }
  @PostMapping("/artigos") public ApiResponse<ArtigoResponse> criar(@Valid @RequestBody ArtigoRequest req, Authentication auth){
    return ApiResponse.ok(service.criar(req, SecurityUtils.currentUserId(auth)));
  }
}