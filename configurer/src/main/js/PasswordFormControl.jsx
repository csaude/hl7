import React, { useState } from "react";
import Button from "react-bootstrap/Button";
import Form from "react-bootstrap/Form";
import InputGroup from "react-bootstrap/InputGroup";

export default function PasswordFormControl({
  id,
  name,
  value,
  onChange,
  required,
}) {
  const [hidden, setHidden] = useState(true);
  return (
    <InputGroup>
      {hidden ? (
        <Form.Control
          id={id}
          name={name}
          type="password"
          value={value}
          onChange={onChange}
          aria-describedby="button-addon"
          required={required}
          autocomplete="new-password"
        />
      ) : (
        <Form.Control
          id={id}
          name={name}
          value={value}
          onChange={onChange}
          aria-describedby="button-addon"
          required={required}
          autocomplete="new-password"
        />
      )}
      <Button
        variant="outline-secondary"
        id="button-addon"
        onClick={() => setHidden(!hidden)}
      >
        👁
      </Button>
      <Form.Control.Feedback type="invalid">
        Por favor insira a senha.
      </Form.Control.Feedback>
    </InputGroup>
  );
}
