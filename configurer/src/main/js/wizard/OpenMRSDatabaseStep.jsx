import React, { useRef, useState } from "react";
import Form from "react-bootstrap/Form";
import { useWizard } from "react-use-wizard";
import PasswordFormControl from "../PasswordFormControl";
import PropTypes from "prop-types";

OpenMRSDatabaseStep.propTypes = {
  configuration: PropTypes.object,
  onConfigurationChange: PropTypes.func,
};

export default function OpenMRSDatabaseStep({
  configuration,
  onConfigurationChange,
}) {
  const [validated, setValidated] = useState(false);
  const { handleStep, nextStep } = useWizard();
  const formRef = useRef(null);

  handleStep(() => {
    setValidated(false);
    if (!formRef.current.checkValidity()) {
      setValidated(true);
      throw new Error("Formulário inválido.");
    }
  });

  function handleConfigurationChange(event) {
    onConfigurationChange({
      [event.target.name]: event.target.value,
    });
  }

  function handleSubmit(event) {
    event.preventDefault();
    nextStep();
  }

  return (
    <Form
      noValidate
      validated={validated}
      ref={formRef}
      onSubmit={handleSubmit}
    >
      <Form.Group className="mb-3">
        <Form.Label htmlFor="dataSourceUrl">URL</Form.Label>
        <Form.Control
          id="dataSourceUrl"
          name="dataSourceUrl"
          value={configuration.dataSourceUrl}
          onChange={handleConfigurationChange}
          autoFocus
          required
        />
        <Form.Text id="passwordHelpBlock" muted>
          🛈 Ex. jdbc:mysql://127.0.0.1:3306/openmrs
        </Form.Text>
        <Form.Control.Feedback type="invalid">
          Por favor insira a URL.
        </Form.Control.Feedback>
      </Form.Group>
      <Form.Group className="mb-3">
        <Form.Label htmlFor="dataSourceUsername">Utilizador</Form.Label>
        <Form.Control
          id="dataSourceUsername"
          name="dataSourceUsername"
          value={configuration.dataSourceUsername}
          onChange={handleConfigurationChange}
          required
        />
        <Form.Control.Feedback type="invalid">
          Por favor insira o utilizador.
        </Form.Control.Feedback>
      </Form.Group>
      <Form.Group className="mb-3">
        <Form.Label htmlFor="dataSourcePassword">Senha</Form.Label>
        <PasswordFormControl
          id="dataSourcePassword"
          name="dataSourcePassword"
          value={configuration.dataSourcePassword}
          onChange={handleConfigurationChange}
          required
        />
      </Form.Group>
      <input type="submit" hidden />
    </Form>
  );
}
