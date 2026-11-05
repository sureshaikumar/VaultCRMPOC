// src/hello-world.jsx
import React from 'react';
import { createRoot } from 'react-dom/client';
import { definePage } from '@veeva/vault';

export default definePage(({ element }) => {
    const root = createRoot(element);
    root.render(
        <div>Hello World!</div>
    );
});