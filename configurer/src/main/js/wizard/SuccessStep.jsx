import React from "react";
import Button from "react-bootstrap/Button";
import { useWizard } from "react-use-wizard";

export default function SuccessStep() {
  const { goToStep } = useWizard();
  function handleClick() {
    goToStep(0);
  }
  return (
    <div className="wizard-success">
      <h1 className="text-center">Configurado com sucesso</h1>
      <p>
        🛈 Deve reiniciar o Tomcat® para aplicar as novas configurações.
      </p>
      <Button onClick={handleClick}>Voltar ao inicio</Button>
    </div>
  );
}
