package br.com.bioimportweb.managedbean;
 
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVReader;

import br.com.bioimportejb.bean.AmostraBean;
import br.com.bioimportejb.bean.interfaces.DadosTaxonLocal;
import br.com.bioimportejb.entidades.Amostra;
import br.com.bioimportejb.entidades.Analise;
import br.com.bioimportejb.entidades.AnaliseBio;
import br.com.bioimportejb.entidades.DadosTaxon;
import br.com.bioimportejb.entidades.Taxon;
import br.com.bioimportejb.util.ChaveSampleVO;
import br.com.bioimportweb.util.Util;
import br.com.daofabrica.excecoes.ExcecaoGenerica;
 
@ViewScoped
@ManagedBean(name="importacaoCsv")
public class ImportacaoCsv implements Serializable {
     
    /**
	 * 
	 */
	private static final long serialVersionUID = -7090737375321788764L;
	private UploadedFile file;
	private File arquivo;
 
	private Logger log = LoggerFactory.getLogger(getClass());
	@EJB
	private AmostraBean sampleBean;
	private List<Amostra> listaSamples;
	
	private static final int PHYLUM = 4;

	private static final int KINGDOM = 3;
	
	private static final int CLASS = 5;
	private static final int ORDER = 6;
	private static final int FAMILY = 7;
	private static final int GENUS = 8;
	private static final int SPECIES = 9;
	private static final int INFRASPECIFICEPITHET = 10;
	private static final int TAXONRANK = 11;
	private static final int SCIENTIFICNAME = 12;
	private static final int COUNTRYCODE = 13;
	private static final int LOCALITY = 14;
	private static final int PUBLISHINGORGKEY = 15;
	private static final int DECIMALLATITUDE = 16;
	private static final int DECIMALLONGITUDE = 17;
	private static final int ELEVATION = 18;
	private static final int ELEVATIONACCURACY = 19;
	private static final int DEPTH = 20;
	private static final int DEPTHACCURACY = 21;
	private static final int EVENTDATE = 22;
	private static final int DAY = 23;
	private static final int MONTH = 24;
	private static final int YEAR = 25;
	private static final int TAXONKEY = 26;
	private static final int SPECIESKEY = 27;
	private static final int BASISOFRECORD = 28;
	private static final int INSTITUTIONCODE = 29;
	private static final int COLLECTIONCODE = 30;
	private static final int CATALOGNUMBER = 31;
	private static final int RECORDNUMBER = 32;
	private static final int IDENTIFIEDBY = 33;
	private static final int RIGHTS = 34;
	private static final int RIGHTSHOLDER = 35;
	private static final int RECORDEDBY = 36;
	private static final int TYPESTATUS = 37;
	private static final int ESTABLISHMENTMEANS = 38;
	private static final int LASTINTERPRETED = 39;
	private static final int MEDIATYPE = 40;
	private static final int ISSUE = 41;
	

	@EJB
	private DadosTaxonLocal dadosTaxonBean;
	
	public Collection<Amostra> lerCsv(InputStream inputStream) {
		Map<ChaveSampleVO, Amostra> samples = new HashMap<ChaveSampleVO, Amostra>(); 
		try {
			 CSVReader reader = new CSVReader(new InputStreamReader(inputStream), '\t');
			 String [] linha;
			 
			 int count = 0;
			 Map<Long, DadosTaxon> dadosTaxon = new HashMap<Long, DadosTaxon>();
			 while ((linha = reader.readNext()) != null) {
				if(count != 0) {
					Amostra sample = new Amostra();
					sample.setDtInclusao(Calendar.getInstance().getTime());
					sample.setUsuario(Util.pegarAtor());
					ChaveSampleVO chave = new ChaveSampleVO();
					int tamanhoLinha = linha.length;
					BigDecimal latitude = null;
					BigDecimal longitude = null;
					if(tamanhoLinha > DECIMALLATITUDE) { 
						latitude = new BigDecimal(linha[DECIMALLATITUDE]);
						sample.setLatitude(latitude);
						chave.setLatitude(latitude);
					}
					if(tamanhoLinha > DECIMALLONGITUDE) {
						longitude = new BigDecimal(linha[DECIMALLONGITUDE]);
						sample.setLongitude(longitude);
						chave.setLongitude(longitude);
					}
					
					if(tamanhoLinha > DEPTH) {
						try {
							String depthString = linha[DEPTH];
							if(depthString != null && !"".equals(depthString)) {
								BigDecimal depth = new BigDecimal(depthString);
								sample.setProfundidade(depth);
								chave.setDepth(depth);
							}
						} catch (Exception e) {
							log.error(e.getMessage(), e);
						}
					}
					
					Date data = dataCsv(linha);
					sample.setDtAmostra(data);
					chave.setData(data);
					
					Amostra aux = samples.get(chave);
					if(aux != null) {
						sample = aux;
					}
					Analise f = new Analise();
					f.setUsuario(Util.pegarAtor());
			    	f.setDtAnalise(Calendar.getInstance().getTime());
			    	
			    	if(f.getAnaliseBio() == null) {
			    		AnaliseBio analiseBio = new AnaliseBio();
			    		analiseBio.setUsuario(Util.pegarAtor());
			    		analiseBio.setDtInclusao(Calendar.getInstance().getTime());
						f.setAnaliseBio(analiseBio);
						Taxon taxon = new Taxon();
						taxon.setUsuario(Util.pegarAtor());
						f.getAnaliseBio().setTaxon(taxon);
						f.getAnaliseBio().setAnalise(f);
						
					}
			    	
			    	/**
			    	 * Sistema recupera o taxonkey da linha corrente
			    	 * E verifica se já recuperou algum dado taxonômico com o mesmo taxonkey
			    	 * Caso já exista recupera do Map, caso não cria um novo para sua persistência.
			    	 */
			    	Long taxonkey = null;
			    	if(TAXONKEY < tamanhoLinha) {
			    		taxonkey = Long.valueOf(linha[TAXONKEY]);
			    	}
			    	
			    	DadosTaxon dTaxon = null;
			    	
			    	/**
			    	 * O sistema verifica se existe algum registro, com o taxonkey recuperado, no banco
			    	 * de dados. Caso exista atribui os valores recebidos neste objeto recuperado, e se caso não exista, 
			    	 * o sistema cria um novo para que futuramente possa ser persistido.
			    	 */
			    	if(taxonkey != null) {
						dTaxon = dadosTaxonBean.buscarPorTaxonKey(taxonkey);
			    	} 
			    	
			    	if(dTaxon == null) {
			    		dTaxon = dadosTaxon.get(taxonkey);
			    	}
			    	
			    	if(dTaxon == null) {
			    		dTaxon = new DadosTaxon();
			    		dTaxon.setTaxonkey(taxonkey);
			    	} 
			    	
					if(KINGDOM < tamanhoLinha) {
			    		dTaxon.setKingdom(linha[KINGDOM]);
			    	}
			    	
			    	if(PHYLUM < tamanhoLinha) {
			    		dTaxon.setPhylum(linha[PHYLUM]);
			    	}
			    	
			    	if(CLASS < tamanhoLinha) {
			    		dTaxon.setClass_(linha[CLASS]);
			    	}
			    	
			    	if(ORDER < tamanhoLinha) {
			    		dTaxon.setOrd(linha[ORDER]);
			    	}
			    	
			    	if(FAMILY < tamanhoLinha) {
			    		dTaxon.setFamily(linha[FAMILY]);
			    	}
			    	
			    	if(GENUS < tamanhoLinha) {
			    		dTaxon.setGenus(linha[GENUS]);
			    	}
			    	
			    	if(SPECIES < tamanhoLinha) {
			    		dTaxon.setSpecies(linha[SPECIES]);
			    	}
			    	
			    	if(INFRASPECIFICEPITHET < tamanhoLinha) {
			    		dTaxon.setInfraspecificepithet(linha[INFRASPECIFICEPITHET]);
			    	}
			    	
			    	if(TAXONRANK < tamanhoLinha) {
			    		dTaxon.setTaxonrank(linha[TAXONRANK]);
			    	}
			    	
			    	if(SCIENTIFICNAME < tamanhoLinha) {
			    		dTaxon.setScientifcname(linha[SCIENTIFICNAME]);
			    	}
			    	
			    	f.getAnaliseBio().getTaxon().setDadosTaxon(dTaxon);
			    	dadosTaxon.put(dTaxon.getTaxonkey(), dTaxon);
			    	
			    	sample.addAnalis(f);
			    	samples.put(chave, sample);
				}
				count ++;
			 }
		} catch (FileNotFoundException e) {
			log.error(e.getMessage(), e);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		} catch (ExcecaoGenerica e) {
			log.error(e.getMessage(), e);
			Util.montaMensagemFlashRedirect("Erro na importação", null);
			Util.validationFailed();
		}
		return samples.values();  
	}
	private Date dataCsv(String[] linha) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); 
		Date data = null;
		try {
			data = sdf.parse(linha[LASTINTERPRETED]);
		} catch (ParseException e) {
			log.error(e.getMessage(), e);
		}
		return data;
	}
	
    public UploadedFile getFile() {
        return file;
    }
 
    public void setFile(UploadedFile file) {
        this.file = file;
    }
     
    public void upload(FileUploadEvent fileUploadEvent) {
        
//        FacesMessage message = new FacesMessage("Succesful", file.getFileName() + " is uploaded.");
//        FacesContext.getCurrentInstance().addMessage(null, message);
        file = fileUploadEvent.getFile();
       
    }
    
    public void importar() {
    	try {
			listaSamples = new ArrayList<Amostra>(); 
			listaSamples.addAll(lerCsv(getFile().getInputstream()));
			listaSamples = sampleBean.salvar(listaSamples);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			Util.montaMensagemErroSemFlashRedirect("Erro ao importar arquivo", null);
		} catch (ExcecaoGenerica e) {
			log.error(e.getMessage(), e);
			Util.montaMensagemErroSemFlashRedirect(e.getMessage(), null);
		}
    }

	public File getArquivo() {
		return arquivo;
	}

	public void setArquivo(File arquivo) {
		this.arquivo = arquivo;
	}

	public List<Amostra> getListaSamples() {
		return listaSamples;
	}

	public void setListaSamples(List<Amostra> listaSamples) {
		this.listaSamples = listaSamples;
	}
	
}