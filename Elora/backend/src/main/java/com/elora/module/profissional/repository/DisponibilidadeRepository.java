public interface DisponibilidadeRepository extends JpaRepository<Disponibilidade,Integer> {
    List<Disponibilidade> findByUsuarioId(Integer id);
    void deleteByUsuarioId(Integer id); // usado em salvarDisponibilidade
}