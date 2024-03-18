import PropTypes from "prop-types";
import React, { useState } from "react";
import Form from "react-bootstrap/Form";
import Spinner from "react-bootstrap/Spinner";
import { useWizard } from "react-use-wizard";
import { fetchConfiguration } from "../api";

WebappFolderStep.propTypes = {
  folder: PropTypes.string.isRequired,
  onFolderChange: PropTypes.func.isRequired,
  onConfigurationChange: PropTypes.func.isRequired,
};

export default function WebappFolderStep({
  folder,
  onFolderChange,
  onConfigurationChange,
}) {
  const { handleStep, nextStep } = useWizard();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  handleStep(async () => {
    if (!folder || folder.length === 0) {
      const e = new Error("Por favor insira a o caminho da pasta.");
      setError(e);
      throw e;
    }

    try {
      setError(null);
      setLoading(true);
      const configuration = await fetchConfiguration(folder);
      onConfigurationChange(configuration);
    } catch (error) {
      setError(error);
      throw error;
    } finally {
      setLoading(false);
    }
  });

  function handleFolderChange(e) {
    setError(null);
    onFolderChange(e.target.value);
  }

  function handleSubmit(event) {
    event.preventDefault();
    nextStep();
  }

  return (
    <Form noValidate onSubmit={handleSubmit}>
      <Form.Group className="mb-3">
        <Form.Label htmlFor="webappFolder">Caminho</Form.Label>
        <Form.Control
          id="webappFolder"
          className={`${error ? "is-invalid" : ""}`}
          name="webappFolder"
          value={folder}
          onChange={handleFolderChange}
          autoFocus
          required
        />
        {!error && (
          <Form.Text id="passwordHelpBlock" muted>
            🛈 Caminho para a pasta da aplicação HL7 dentro da pasta webapps do
            Tomcat®
          </Form.Text>
        )}
        {loading && <Spinner animation="border" />}
        <Form.Control.Feedback type="invalid">
          {error?.message}
        </Form.Control.Feedback>
      </Form.Group>
    </Form>
  );
}
