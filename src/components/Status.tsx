export function Loading({ what }: { what: string }) {
  return <p className="muted">Loading {what}…</p>
}

export function ErrorBox({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <div className="error">
      <p>Failed to load: {message}</p>
      <button onClick={onRetry}>Retry</button>
    </div>
  )
}

export function Empty({ what }: { what: string }) {
  return <p className="muted">No {what} found.</p>
}

export function Pager({
  page,
  totalPages,
  onPage,
}: {
  page: number
  totalPages: number
  onPage: (p: number) => void
}) {
  if (totalPages <= 1) return null
  return (
    <div className="pager">
      <button disabled={page === 0} onClick={() => onPage(page - 1)}>
        Prev
      </button>
      <span>
        Page {page + 1} of {totalPages}
      </span>
      <button disabled={page + 1 >= totalPages} onClick={() => onPage(page + 1)}>
        Next
      </button>
    </div>
  )
}
