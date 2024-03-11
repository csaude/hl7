import PropTypes from "prop-types";
import React, { useRef, useState } from "react";
import Form from "react-bootstrap/Form";
import { useWizard } from "react-use-wizard";
import PasswordFormControl from "../PasswordFormControl";
import { AuthenticationError, AuthorizationError, fetchKeyStore } from "../api";

SecurityStep.propTypes = {
  configuration: PropTypes.object,
  onConfigurationChange: PropTypes.func,
};

export default function SecurityStep({ configuration, onConfigurationChange }) {
  const [validated, setValidated] = useState(false);
  const [errors, setErrors] = useState({});
  const { handleStep, nextStep } = useWizard();
  const formRef = useRef(null);

  handleStep(async () => {
    setValidated(false);
    if (!formRef.current.checkValidity()) {
      setValidated(true);
      throw new Error("Formulário inválido.");
    }
    const err = {};
    try {
      await fetchKeyStore(
        configuration.keyStorePath,
        configuration.keyStorePassword
      );
    } catch (error) {
      if (error instanceof AuthenticationError) {
        err.keyStorePassword = error;
      } else if (error instanceof AuthorizationError) {
        err.keyStorePath = error;
      }
      setErrors(err);
      throw error;
    }
  });

  function handleConfigurationChange(event) {
    onConfigurationChange({
      [event.target.name]: event.target.value,
    });
  }

  async function handleKeyStoreBlur() {
    if (
      configuration.keyStorePath?.length === 0 ||
      configuration.keyStorePassword?.length === 0
    ) {
      return;
    }

    const err = {};
    try {
      setErrors({});
      const keyStore = await fetchKeyStore(
        configuration.keyStorePath,
        configuration.keyStorePassword
      );

      if (keyStore.disaSecretKeyAlias) {
        onConfigurationChange({
          ...configuration,
          disaSecretKey: keyStore.disaSecretKeyAlias,
        });
      }
    } catch (error) {
      if (error instanceof AuthenticationError) {
        err.keyStorePassword = error;
      } else if (error instanceof AuthorizationError) {
        err.keyStorePath = error;
      }
      setErrors(err);
    }
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
        <Form.Label htmlFor="keyStorePath">Key store</Form.Label>
        <Form.Control
          id="keyStorePath"
          name="keyStorePath"
          className={`${errors.keyStorePath ? "is-invalid" : ""}`}
          value={configuration.keyStorePath}
          onChange={handleConfigurationChange}
          onBlur={handleKeyStoreBlur}
          autoFocus
          required
          autoComplete="new-password"
        />
        <Form.Control.Feedback type="invalid">
          {errors.keyStorePath?.message ||
            "Por favor insira o caminho para o ficheiro key store."}
        </Form.Control.Feedback>
      </Form.Group>
      <Form.Group className="mb-3">
        <Form.Label htmlFor="keyStorePassword">Password</Form.Label>
        <PasswordFormControl
          id="keyStorePassword"
          name="keyStorePassword"
          className={`${errors.keyStorePassword ? "is-invalid" : ""}`}
          value={configuration.keyStorePassword}
          error={errors.keyStorePassword}
          onChange={handleConfigurationChange}
          onBlur={handleKeyStoreBlur}
          required
        />
      </Form.Group>
      <Form.Group className="mb-3">
        <Form.Label htmlFor="disaSecretKey">DISA secret key</Form.Label>
        <PasswordFormControl
          id="disaSecretKey"
          name="disaSecretKey"
          value={configuration.disaSecretKey}
          onChange={handleConfigurationChange}
          required
        />
      </Form.Group>
      <input type="submit" hidden />
    </Form>
  );
}
