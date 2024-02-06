import React, { useRef, useState } from "react";
import Form from "react-bootstrap/Form";
import { useWizard } from "react-use-wizard";
import PasswordFormControl from "../PasswordFormControl";

export default function OpenMRSAPIStep({
  configuration,
  onConfigurationChange,
}) {
  const [validated, setValidated] = useState(false);
  const [hidden, setHidden] = useState(true);
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
      ...configuration,
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
      onSubmit={handleSubmit}
      ref={formRef}
    >
      <Form.Group className="mb-3">
        <Form.Label htmlFor="openmrsUrl">URL</Form.Label>
        <Form.Control
          id="openmrsUrl"
          name="openmrsUrl"
          value={configuration.openmrsUrl}
          onChange={handleConfigurationChange}
          autoFocus
          required
        />
        <Form.Text id="passwordHelpBlock" muted>
          Ex. http://127.0.0.1:8080/openmrs
        </Form.Text>
        <Form.Control.Feedback type="invalid">
          Por favor insira a URL.
        </Form.Control.Feedback>
      </Form.Group>
      <Form.Group className="mb-3">
        <Form.Label htmlFor="openmrsUsername">Utilizador</Form.Label>
        <Form.Control
          id="openmrsUsername"
          name="openmrsUsername"
          value={configuration.openmrsUsername}
          onChange={handleConfigurationChange}
          required
        />
        <Form.Control.Feedback type="invalid">
          Por favor insira o utilizador.
        </Form.Control.Feedback>
      </Form.Group>
      <Form.Group className="mb-3">
        <Form.Label htmlFor="openmrsPassword">Senha</Form.Label>
        <PasswordFormControl
          id="openmrsPassword"
          name="openmrsPassword"
          value={configuration.openmrsPassword}
          onChange={handleConfigurationChange}
          required
        />
      </Form.Group>
      <input type="submit" hidden />
    </Form>
  );
}
