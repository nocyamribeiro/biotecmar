package br.com.bioimportejb.enumerator;


public enum PerfilEnum {
	
	AD("AD", "Administrador");
	
	
	private PerfilEnum(String sigla, String descricao) {
		this.sigla = sigla;
		this.descricao = descricao;
	}

	private String sigla;
	private String descricao;
	
	public String getSigla() {
		return sigla;
	}
	public void setSigla(String sigla) {
		this.sigla = sigla;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	@Override
	public String toString() {
		return this.sigla;
	}
}
