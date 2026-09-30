ALTER TABLE profissional_detalhes ADD COLUMN IF NOT EXISTS status_verificacao ENUM('pendente','em_analise','aprovado','rejeitado','correcao') NOT NULL DEFAULT 'pendente';
ALTER TABLE profissional_detalhes ADD COLUMN IF NOT EXISTS motivo_rejeicao TEXT NULL;

CREATE TABLE IF NOT EXISTS validacao_profissional (
  id_validacao INT AUTO_INCREMENT PRIMARY KEY,
  profissional_id INT NOT NULL,
  validador_id INT NULL,
  resultado ENUM('aprovado','reprovado','correcao') NOT NULL,
  observacao TEXT NULL,
  data_validacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (profissional_id) REFERENCES usuario(id_usuario) ON DELETE CASCADE,
  FOREIGN KEY (validador_id) REFERENCES usuario(id_usuario)
);