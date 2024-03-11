import PropTypes from "prop-types";
import React, { useRef, useState } from "react";
import Alert from "react-bootstrap/Alert";
import Form from "react-bootstrap/Form";
import { useWizard } from "react-use-wizard";
import PasswordFormControl from "../PasswordFormControl";
import { ValidationError, saveConfiguration } from "../api";

WebappAuthenticationStep.propTypes = {
  folder: PropTypes.string.isRequired,
  configuration: PropTypes.object,
  onConfigurationChange: PropTypes.func,
};

export default function WebappAuthenticationStep({
  folder,
  configuration,
  onConfigurationChange,
}) {
  const [validated, setValidated] = useState(false);
  const [error, setError] = useState(null);
  const { handleStep, nextStep } = useWizard();
  const formRef = useRef(null);

  function handleConfigurationChange(event) {
    onConfigurationChange({
      [event.target.name]: event.target.value,
    });
  }

  handleStep(async () => {
    setError(false);
    setValidated(false);
    if (!formRef.current.checkValidity()) {
      setValidated(true);
      throw new Error("Formulário inválido.");
    }
    try {
      await saveConfiguration(folder, configuration);
    } catch (error) {
      setError(error);
      throw error;
    }
  });

  function handleSubmit(event) {
    event.preventDefault();
    nextStep();
  }

  function isValidationError() {
    return error instanceof ValidationError;
  }

  return (
    <Form
      noValidate
      validated={validated}
      ref={formRef}
      onSubmit={handleSubmit}
    >
      {error && isValidationError() && (
        <Alert variant="danger">
          Não foi possível gravar a configuração. Certifique que todos os campos
          foram devidamente preenchidos.
        </Alert>
      )}

      {error && !isValidationError() && (
        <Alert variant="danger">
          Ocorreu um erro inesperado. Por favor tente novamente.
        </Alert>
      )}

      <Form.Group className="mb-3">
        <Form.Label htmlFor="appUsername">Utilizador</Form.Label>
        <Form.Control
          id="appUsername"
          name="appUsername"
          value={configuration.appUsername}
          onChange={handleConfigurationChange}
          autoFocus
          required
        />
        <Form.Control.Feedback type="invalid">
          Por favor insira o utilizador.
        </Form.Control.Feedback>
      </Form.Group>
      <Form.Group className="mb-3">
        <Form.Label htmlFor="appPassword">Senha</Form.Label>
        <PasswordFormControl
          id="appPassword"
          name="appPassword"
          value={configuration.appPassword}
          onChange={handleConfigurationChange}
          required
        />
      </Form.Group>
      <input type="submit" hidden />
    </Form>
  );
}
