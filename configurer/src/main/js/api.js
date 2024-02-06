class ValidationError extends Error {
  constructor(data, ...params) {
    super(...params);

    if (Error.captureStackTrace) {
      Error.captureStackTrace(this, ValidationError);
    }

    this.name = "ValidationError";
    this.data = data;
  }
}

async function fetchConfiguration(folder) {
  const response = await fetch(`/configuration?folder=${folder}`);
  if (response.status === 403) {
    throw new Error(
      "Não tem permissões suficentes para abrir o ficheiro de configuração."
    );
  }
  if (response.status === 404) {
    throw new Error(
      "Não foi encontrado o ficheiro de configuração. Certifique que a aplicação foi implantada com sucesso."
    );
  }
  if (response.status != 200) {
    throw new Error("Não foi possível carregar o ficheiro de configurações.");
  }
  return response.json();
}

async function fetchWebappFolder() {
  const response = await fetch("/folder");
  if (response.status != 200) {
    throw new Error("Não foi possível aceder a pasta da ferramenta HL7.");
  }
  return response.text();
}

async function saveConfiguration(folder, configuration) {
  const response = await fetch(`/configuration?folder=${folder}`, {
    headers: {
      "Content-Type": "application/json",
    },
    method: "POST",
    body: JSON.stringify(configuration),
  });

  if (response.status === 400) {
    const data = await response.json();
    throw new ValidationError(
      data,
      "Alguns campos possuem erros de validação."
    );
  }

  if (response.status != 200) {
    throw new Error(
      "Não foi possível salvar o ficheiro application.properties. Verifique os logs do servidor."
    );
  }
}
export {
  fetchConfiguration,
  fetchWebappFolder,
  saveConfiguration,
  ValidationError,
};
