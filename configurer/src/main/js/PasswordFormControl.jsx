import PropTypes from "prop-types";
import React, { useState } from "react";
import Button from "react-bootstrap/Button";
import Form from "react-bootstrap/Form";
import InputGroup from "react-bootstrap/InputGroup";

PasswordFormControl.propTypes = {
  id: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
  value: PropTypes.string.isRequired,
  error: PropTypes.object,
  onChange: PropTypes.func.isRequired,
  onBlur: PropTypes.func,
  required: PropTypes.bool,
};

export default function PasswordFormControl({
  id,
  name,
  label,
  value,
  error,
  onChange,
  onBlur,
  required,
}) {
  const [hidden, setHidden] = useState(true);
  return (
    <InputGroup>
      {hidden ? (
        <Form.Control
          id={id}
          className={`${error ? "is-invalid" : ""}`}
          name={name}
          type="password"
          value={value}
          onChange={onChange}
          onBlur={onBlur}
          aria-describedby={`${id}-button-addon`}
          required={required}
          autoComplete="off"
        />
      ) : (
        <Form.Control
          id={id}
          className={`${error ? "is-invalid" : ""}`}
          name={name}
          value={value}
          onChange={onChange}
          onBlur={onBlur}
          aria-describedby={`${id}-button-addon`}
          required={required}
          autoComplete="off"
        />
      )}
      <Button
        variant="outline-secondary"
        id={`${id}-button-addon`}
        onClick={() => setHidden(!hidden)}
      >
        👁
      </Button>
      <Form.Control.Feedback type="invalid">
        {error?.message || `Por favor insira a ${label ? label : "senha"}.`}
      </Form.Control.Feedback>
    </InputGroup>
  );
}
