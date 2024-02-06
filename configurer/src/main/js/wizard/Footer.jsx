import React from "react";
import Button from "react-bootstrap/Button";
import { useWizard } from "react-use-wizard";

export default function Footer() {
  const {
    previousStep,
    nextStep,
    isFirstStep,
    isLastStep,
    activeStep,
    stepCount,
  } = useWizard();

  return (
    <div className="wizard-footer">
      {!isFirstStep && !isLastStep && (
        <Button className="me-1" variant="light" onClick={() => previousStep()}>
          ◂ Anterior
        </Button>
      )}

      {activeStep < stepCount - 2 && (
        <Button variant="primary" onClick={() => nextStep()}>
          Próximo ▸
        </Button>
      )}

      {activeStep === stepCount - 2 && (
        <Button variant="primary" onClick={() => nextStep()}>
          Finalizar
        </Button>
      )}
    </div>
  );
}
