import React from "react";
import { useWizard } from "react-use-wizard";
import { steps } from "./steps";

export default function Header() {
  const { activeStep, isLastStep } = useWizard();
  return (
    <div className="wizard-header">
      {!isLastStep &&
        steps.slice(0, steps.length - 1).map((s, i) => (
          <span key={s.name} className={activeStep === i ? "active" : ""}>
            {i + 1} {s.name}
          </span>
        ))}
    </div>
  );
}
