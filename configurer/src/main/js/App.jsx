import "bootstrap/dist/css/bootstrap.min.css";
import React, { useEffect, useState } from "react";
import Spinner from "react-bootstrap/Spinner";

import { Wizard } from "react-use-wizard";
import { fetchWebappFolder } from "./api";
import Footer from "./wizard/Footer";
import Header from "./wizard/Header";
import { steps } from "./wizard/steps";

export default function App() {
  const [loading, setLoading] = useState(false);
  const [folder, setFolder] = useState("");
  const [configuration, setConfiguration] = useState(null);

  // Try to find pre-defined folders
  useEffect(() => {
    async function doFetch() {
      try {
        setLoading(true);
        const f = await fetchWebappFolder();
        setFolder(f);
      } catch (error) {
        // No problem if it fails, let the user choose the folder.
        console.log(error);
      } finally {
        setLoading(false);
      }
    }
    doFetch();
  }, []);

  return (
    <div className="wizard">
      {loading ? (
        <Spinner animation="border" />
      ) : (
        <Wizard header={<Header />} footer={<Footer />}>
          {steps.map((s) => (
            <s.component
              key={s.name}
              folder={folder}
              configuration={configuration}
              onFolderChange={setFolder}
              onConfigurationChange={setConfiguration}
            />
          ))}
        </Wizard>
      )}
    </div>
  );
}
