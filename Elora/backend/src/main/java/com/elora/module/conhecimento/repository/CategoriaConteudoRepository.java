@Service @RequiredArgsConstructor
public class ConhecimentoService {
  public ArtigoResponse criar(ArtigoRequest req, Integer autorId){
    if(!usuarioService.ehStaff(autorId)) throw new ForbiddenException("Apenas equipe");
    var cat = req.getCategoriaId()!=null ? categorias.findById(req.getCategoriaId()).orElseThrow() : null;
    // save + toResponse
  }
  public List<ArtigoResponse> listarPublicados(){
    return List.of();
  }
}